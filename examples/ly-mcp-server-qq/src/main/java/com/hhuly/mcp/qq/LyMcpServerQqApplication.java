package com.hhuly.mcp.qq;

import com.hhuly.mcp.qq.tools.QQTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LyMcpServerQqApplication {

	public static void main(String[] args) {
		SpringApplication.run(LyMcpServerQqApplication.class, args);
	}

	/**
	 * 注册工具回调（Tool Callbacks）
	 * @param qqTool
	 * @return
	 */
	@Bean
	public ToolCallbackProvider qqTools(QQTool qqTool) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(qqTool)
				.build();
	}
}
