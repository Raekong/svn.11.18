package edu.nuist.ojs.middle;


import edu.nuist.ojs.middle.stub.CallStub;

import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;

import cn.hutool.crypto.SecureUtil;

public class TestContext {
    @Autowired
    CallStub callStub;

    @Test
    public void t() {
        List users = callStub.callStub("getUserByEmailAndPid", List.class, "email", "2846", "pid", 1);

    }

}
