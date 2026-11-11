package net.interplay.simplestoragemanager.client.gui;

/** All terminal texture coordinates are kept here so SMALL and BIG stay aligned. */
public final class TerminalScreenLayout {
    public static final int SLOT_SIZE = 18;
    public static final int ITEM_OFFSET = 1;
    public static final int MAIN_COLUMNS = 9;
    public static final int BIG_VERTICAL_OFFSET = 111;
    public static final int MAIN_CONTENT_INSET = 1;
    public static final int BUTTON_SIZE = 8;
    public static final int LOCK_BUTTON_SIZE = 10;
    public static final int ALERT_ICON_SIZE = 10;
    public static final int SEARCH_LEFT_INSET = 1;
    /** Horizontal limit of every terminal section title box; titles scroll inside it. */
    public static final int TITLE_BOX_WIDTH = 131;

    public static final Variant SMALL = new Variant(
            216, 230,
            new Rect(31, 23, 192, 65),
            new Rect(198, 23, 210, 141),
            new Rect(103 + SEARCH_LEFT_INSET, 8, 192, 19),
            new Point(6, 5), new Point(6, 23), new Point(6, 41),
            new Rect(24, 70, 74, 141),
            new Point[]{new Point(7, 70), new Point(7, 88), new Point(7, 106), new Point(7, 124)},
            new Point(75, 124), new Point(103, 80), new Point(175, 98), new Point(156, 70), new Point(83, 70),
            new Point(31, 147), new Point(31, 205));

    public static final Variant BIG = new Variant(
            216, 342,
            new Rect(31, 23, 192, 177),
            new Rect(198, 23, 210, 141),
            new Rect(103 + SEARCH_LEFT_INSET, 8, 192, 19),
            new Point(6, 5), new Point(6, 23), new Point(6, 41),
            new Rect(24, 181, 74, 252),
            new Point[]{new Point(7, 181), new Point(7, 199), new Point(7, 217), new Point(7, 235)},
            new Point(75, 235), new Point(103, 191), new Point(175, 209), new Point(156, 182), new Point(83, 182),
            new Point(31, 258), new Point(31, 316));

    private TerminalScreenLayout() {
    }

    public static int scrollRange(Variant variant, int contentHeight) {
        return Math.max(0, contentHeight - variant.contentArea().height());
    }

    public record Point(int x, int y) {
        public Point shifted(int dy) {
            return new Point(x, y + dy);
        }
    }

    public record Rect(int left, int top, int right, int bottom) {
        public int width() {
            return right - left + 1;
        }

        public int height() {
            return bottom - top + 1;
        }

        public Rect inset(int amount) {
            return new Rect(left + amount, top + amount, right - amount, bottom - amount);
        }
    }

    public record Variant(int imageWidth, int imageHeight, Rect mainArea, Rect scrollTrack, Rect searchArea,
                          Point upgrade3, Point upgrade2, Point upgrade1, Rect playerPortrait,
                          Point[] armor, Point offhand, Point craftOrigin, Point result, Point cross,
                          Point lockButton, Point inventoryOrigin, Point hotbarOrigin) {
        public Rect contentArea() {
            return mainArea.inset(MAIN_CONTENT_INSET);
        }
    }
}
