package com.saju.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saju.service.SajuService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class SajuController {
	
	private final SajuService sajuService;
	
	@GetMapping
	public String test() throws Exception{
		return sajuService.calculateSaju();
	}

}
