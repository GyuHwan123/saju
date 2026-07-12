package com.saju.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service

public class SajuService {

	private final PythonClient pythonClient;
	
	
	public String calculateSaju() throws Exception{
		return pythonClient.calculateSaju();
	}

}
