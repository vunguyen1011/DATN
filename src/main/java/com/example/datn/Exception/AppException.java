    package com.example.datn.Exception;

    import lombok.Getter;
    import lombok.Setter;

    @Getter
    @Setter
    public class AppException extends RuntimeException{

        private ErrorCode errorCode;
        public AppException(ErrorCode errorCode) {
            super(errorCode.getMessage());
            this.errorCode = errorCode;
        }
        public AppException(ErrorCode errorCode, String customMessage) {
            super(customMessage);
            this.errorCode = errorCode;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this; // Tắt stack trace để tối ưu hóa CPU trong High Concurrency
        }

    }