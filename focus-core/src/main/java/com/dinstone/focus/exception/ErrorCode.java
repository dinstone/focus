package com.dinstone.focus.exception;

public enum ErrorCode {

	UNKOWN_ERROR(1000), INVOKE_ERROR(599), CODEC_ERROR(201), METHOD_ERROR(204), DECLARED_ERROR(301),
	UNDECLARED_ERROR(302), RUNTIME_ERROR(303), PARAM_ERROR(304), ACCESS_ERROR(305), TIMEOUT_ERROR(306);

	private int code;

	private ErrorCode(int value) {
		this.code = value;
	}

	public int value() {
		return code;
	}

	public static ErrorCode valueOf(int code) {
		for (ErrorCode errorCode : ErrorCode.values()) {
			if (errorCode.code == code) {
				return errorCode;
			}
		}
		return UNKOWN_ERROR;
	}
}
