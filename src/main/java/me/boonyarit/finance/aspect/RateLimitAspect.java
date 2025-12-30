package me.boonyarit.finance.aspect;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.function.Supplier;
import me.boonyarit.finance.annotation.RateLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class RateLimitAspect {

    private final ProxyManager<String> proxyManager;

    @Autowired
    public RateLimitAspect(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Around("@annotation(me.boonyarit.finance.annotation.RateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RateLimit rateLimit = signature.getMethod().getAnnotation(RateLimit.class);

        String key = resolveKey(rateLimit.key());

        Supplier<BucketConfiguration> configurationSupplier = getConfigSupplier(rateLimit);

        ConsumptionProbe probe = proxyManager
            .builder()
            .build(key, configurationSupplier)
            .tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            addHeaders(probe.getRemainingTokens(), rateLimit);
            return joinPoint.proceed();
        } else {
            long waitForRefillSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            addResetHeader(waitForRefillSeconds);
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded. Try again in " + waitForRefillSeconds + " seconds."
            );
        }
    }

    private Supplier<BucketConfiguration> getConfigSupplier(RateLimit rateLimit) {
        return () ->
            BucketConfiguration.builder()
                .addLimit(
                    Bandwidth.builder()
                        .capacity(rateLimit.capacity())
                        .refillIntervally(rateLimit.capacity(), Duration.of(1, rateLimit.timeUnit().toChronoUnit()))
                        .build()
                )
                .build();
    }

    private String resolveKey(String annotationKey) {
        String userIp = "unknown";
        try {
            HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                userIp = xForwardedFor.split(",")[0].trim();
            } else {
                userIp = request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Fallback for non-web contexts or testing
        }

        return annotationKey + ":" + userIp;
    }

    private void addHeaders(long remainingTokens, RateLimit rateLimit) {
        HttpServletResponse response =
            ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
        if (response != null) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));
            response.addHeader("X-Rate-Limit-Limit", String.valueOf(rateLimit.capacity()));
        }
    }

    private void addResetHeader(long waitForRefillSeconds) {
        try {
            HttpServletResponse response =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getResponse();
            if (response != null) {
                response.addHeader("Retry-After", String.valueOf(waitForRefillSeconds));
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefillSeconds));
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}
