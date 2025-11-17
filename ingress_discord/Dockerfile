# escape=\
# syntax=docker/dockerfile:1

ARG RUST_VERSION=1.91.0
ARG ALPINE_VERSION

FROM rust:${RUST_VERSION}-alpine${ALPINE_VERSION} AS build

WORKDIR /app

# Install host build dependencies.
RUN apk add --no-cache \
    clang \
    lld \
    musl-dev \
    git

ARG VERSION
ENV VERSION=${VERSION:-dev}

# Build the application.
# Leverage a cache mount to /usr/local/cargo/registry/
# for downloaded dependencies, a cache mount to /usr/local/cargo/git/db
# for git repository dependencies, and a cache mount to /app/target/ for
# compiled dependencies which will speed up subsequent builds.
# Leverage a bind mount to the src directory to avoid having to copy the
# source code into the container. Once built, copy the executable to an
# output directory before the cache mounted /app/target is unmounted.
RUN --mount=type=bind,source=src,target=src \
    --mount=type=bind,source=Cargo.toml,target=Cargo.toml \
    --mount=type=bind,source=Cargo.lock,target=Cargo.lock \
    --mount=type=bind,source=build.rs,target=build.rs \
    --mount=type=bind,source=migrations,target=migrations \
    --mount=type=bind,source=diesel.toml,target=diesel.toml \
    --mount=type=cache,target=/app/target/ \
    --mount=type=cache,target=/usr/local/cargo/git/db \
    --mount=type=cache,target=/usr/local/cargo/registry/ \
cargo build --locked --release --bins && \
cp ./target/release/ingress /bin/ingress && \
cp ./target/release/register /bin/register

################################################################################
# Create a new stage for running the application that contains the minimal
# runtime dependencies for the application.
FROM alpine:${ALPINE_VERSION:-latest} AS deploy

# Create a non-privileged user that the app will run under.
# See https://docs.docker.com/go/dockerfile-user-best-practices/
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser

COPY --from=build /bin/ingress /bin/ingress
COPY --from=build /bin/register /bin/register

EXPOSE 3000

ENTRYPOINT ["/bin/ingress"]
