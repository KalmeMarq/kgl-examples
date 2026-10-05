package io.github.kalmemarq.kgl.api.window;

public record WindowConfig(String title, int width, int height, int minWidth, int minHeight, int desiredWidth, int desiredHeight, boolean resizable) {
    public WindowConfig() {
        this("", 800, 600, 1, 1, 800, 600, true);
    }

    public WindowConfig withTitle(String title) {
        return new WindowConfig(title, this.width, this.height, this.minWidth, this.minHeight, this.desiredWidth, this.desiredHeight, this.resizable);
    }

    public WindowConfig withSize(int width, int height) {
        return new WindowConfig(this.title, width, height, this.minWidth, this.minHeight, this.desiredWidth, this.desiredHeight, this.resizable);
    }

    public WindowConfig withMinSize(int minWidth, int minHeight) {
        return new WindowConfig(this.title, this.width, this.height, minWidth, minHeight, this.desiredWidth, this.desiredHeight, this.resizable);
    }

    public WindowConfig withDesiredSize(int desiredWidth, int desiredHeight) {
        return new WindowConfig(this.title, this.width, this.height, this.minWidth, this.minHeight, desiredWidth, desiredHeight, this.resizable);
    }

    public WindowConfig withResizable(boolean resizable) {
        return new WindowConfig(this.title, this.width, this.height, this.minWidth, this.minHeight, this.desiredWidth, this.desiredHeight, resizable);
    }
}
