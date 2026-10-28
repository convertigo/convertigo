## AOT cache of the JVM, trained without project when the image is built (native platforms only,
## not emulated ones; --build-arg AOT_CACHE_IMAGE=false skips it) and used by default by the
## containers, unless started with AOT_CACHE=off

COPY ./aot-train.sh /
ARG AOT_CACHE_IMAGE=true
ARG BUILDPLATFORM
ARG TARGETPLATFORM
RUN if [ "$AOT_CACHE_IMAGE" = "true" ]; then \
        if [ "$TARGETPLATFORM" = "$BUILDPLATFORM" ]; then \
            /aot-train.sh; \
        else \
            echo "AOT training skipped: $TARGETPLATFORM is emulated on $BUILDPLATFORM"; \
        fi; \
    fi
