package com.example.datn.Config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelConfig {

    /**
     * Đăng ký AspectJ để @SentinelResource hoạt động.
     * Standalone sentinel-core không có Spring Cloud auto-config,
     * tránh hoàn toàn lỗi ClassNotFoundException với Spring Boot 4.x.
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setResource("enrollment_api");
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // Đặt giới hạn chịu tải tối đa: 1000 QPS cho API đăng ký
        // Khi traffic vượt ngưỡng này, Sentinel tự Load Shedding (fast-fail)
        rule.setCount(2000);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }
}
