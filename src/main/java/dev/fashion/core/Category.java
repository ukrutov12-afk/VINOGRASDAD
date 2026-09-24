package dev.fashion.core;

public enum Category {
    COMBAT("Бой", "Сражение и цели", '\uE000'),
    MOVEMENT("Движение", "Перемещение персонажа", '\uE001'),
    RENDER("Отрисовка", "Интерфейс поверх мира", '\uE002'),
    PLAYER("Игрок", "Действия персонажа", '\uE003'),
    MISC("Прочее", "Всё остальное", '\uE004');

    public final String title;
    public final String subtitle;
    public final char icon;

    Category(String title, String subtitle, char icon) {
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
    }
}
