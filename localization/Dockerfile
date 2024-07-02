FROM golang:1.22.1 as builder
ARG CGO_ENABLED=0
WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download
COPY . .
EXPOSE 50052
EXPOSE 8081
RUN go build -o exec_localization

FROM scratch
COPY --from=builder /app/exec_localization /exec_localization
EXPOSE 50052
EXPOSE 8081
ENTRYPOINT ["/exec_localization"]