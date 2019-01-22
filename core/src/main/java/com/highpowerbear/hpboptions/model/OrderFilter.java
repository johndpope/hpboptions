package com.highpowerbear.hpboptions.model;

/**
 * Created by robertk on 1/22/2019.
 */
public class OrderFilter {

    private boolean showNew = true;
    private boolean showWorking = true;
    private boolean showCompleted = true;

    public boolean isShowNew() {
        return showNew;
    }

    public void setShowNew(boolean showNew) {
        this.showNew = showNew;
    }

    public boolean isShowWorking() {
        return showWorking;
    }

    public void setShowWorking(boolean showWorking) {
        this.showWorking = showWorking;
    }

    public boolean isShowCompleted() {
        return showCompleted;
    }

    public void setShowCompleted(boolean showCompleted) {
        this.showCompleted = showCompleted;
    }
}
