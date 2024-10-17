FROM rust:1.81.0 AS build

WORKDIR /build

RUN apt update \
    && apt install -y \
    musl-dev \
    libssl-dev \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

# statically link against openssl
ENV OPENSSL_STATIC=1

ARG RUST_ENVIRONMENT
ENV RUST_ENVIRONMENT=${RUST_ENVIRONMENT:-production}

ARG VERSION
ENV VERSION=${VERSION:-dev}

COPY . .

RUN --mount=type=cache,target=/usr/local/cargo/registry \
    --mount=type=cache,target=/home/root/app/target \
    cargo build --target x86_64-unknown-linux-gnu --release --bins

FROM debian:12.7-slim AS runtime

WORKDIR /

RUN apt update \
    && apt install -y \
    libssl-dev \
    pkg-config \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/x86_64-unknown-linux-gnu/release/ingress .
COPY --from=build /build/target/x86_64-unknown-linux-gnu/release/register .

EXPOSE 3000

ENTRYPOINT ["./ingress"]
