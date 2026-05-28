package com.baidu.bikenavi_demo.func;

import java.util.List;

public class DEMOFuncBean {
    public final List<DEMOFuncBeanChild> childList;
    private final String funcName;
    public String getFuncName() {
        return funcName;
    }

    public DEMOFuncBean(List<DEMOFuncBeanChild> childList, String funcName) {
        this.childList = childList;
        this.funcName = funcName;
    }

    public static class DEMOFuncBeanChild {
        private String childFuncName;
        public String getFuncName() {
            return childFuncName;
        }

        public DEMOFuncBeanChild(String childFuncName) {
            this.childFuncName = childFuncName;
        }
    }
}
