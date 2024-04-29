FROM golang:1.22.1 as builder
ARG CGO_ENABLED=0
WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download
COPY . .
EXPOSE 50051
RUN go build -o server

FROM scratch
COPY --from=builder /app/server /server
EXPOSE 50051
ENTRYPOINT ["/server"]