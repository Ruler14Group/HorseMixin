package cn.moy84.horse.interfaces;

public interface HorseMixinInterface {
    default int getValueThirst(){
        return 0;
    }
    default void setValueThirst(int valueThirst) {
    }
    default void onSprint() {}
}
