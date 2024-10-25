FROM rust:1.81.0-alpine AS base

RUN apk update && apk add --no-cache musl-dev

RUN cargo install cargo-chef --version ^0.1

WORKDIR /app

ARG RUST_ENVIRONMENT
ENV RUST_ENVIRONMENT=${RUST_ENVIRONMENT:-production}

FROM base AS planner

COPY . .

RUN --mount=type=cache,target=/usr/local/cargo/registry \
    cargo chef prepare --recipe-path recipe.json

FROM base AS builder

COPY --from=planner /app/recipe.json recipe.json

RUN --mount=type=cache,target=/usr/local/cargo/registry \
    cargo chef cook --release --recipe-path recipe.json

ARG VERSION
ENV VERSION=${VERSION:-dev}

COPY . .

RUN --mount=type=cache,target=/usr/local/cargo/registry \
    cargo build --release --bins

FROM alpine:latest AS runtime

WORKDIR /

COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/

COPY --from=builder /app/target/release/ingress .
COPY --from=builder /app/target/release/register .

EXPOSE 3000

ENTRYPOINT ["./ingress"]
