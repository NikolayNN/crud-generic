package by.nhorushko.crudgeneric.flex.model;

public class SettingsTranslateable implements SettingsVoid {

    protected String lang;
    protected Object cacheLanguage;

    public Object getCachedLanguage(){
        return cacheLanguage;
    }

    public void setCachedLanguage(Object o) {
        this.cacheLanguage = o;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
