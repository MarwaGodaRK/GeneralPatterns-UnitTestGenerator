package com.aurea.testgenerator.pattern;

public class EntityMethodExample {

    public GnxBusinessEntityImpl getJP_Abn() {
        return (GnxBusinessEntityImpl) getEntity(0);
    }

    private GnxBusinessEntityImpl getEntity(int i) {
        return null;
    }

    private static class GnxBusinessEntityImpl {
    }
}
