package com.model;

public class PasswordNotStrongException extends RuntimeException{
	public PasswordNotStrongException(String msg){
		super(msg);
	}
}
