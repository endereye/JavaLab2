package cn.endereye.framework.ioc.test.b;

import cn.endereye.framework.ioc.annotations.InjectSource;

@InjectSource
public class BImplB implements B {
    @Override
    public String getMessageB() {
        return "b-impl-b";
    }
}
