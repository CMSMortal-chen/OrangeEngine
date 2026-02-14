package cms.mortalchen.orange.render.font;

public class FontManager {
    public long nvgContext;
    public static Font exo;
    public static Font montserrat;
    public static Font montserratBold;
    public static Font hmSans_CHS;
    public static Font sourceHan_CHS;
    public static Font geometos;
    public static Font sourceSerif_CHS;
    public static Font jetBrainsMono;

    public FontManager(long nvgContext) {
        this.nvgContext = nvgContext;
        exo = new Font(nvgContext, "Exo-Regular", "otf");
        hmSans_CHS = new Font(nvgContext, "HarmonyOS_Sans_SC_Regular");
        sourceHan_CHS = new Font(nvgContext, "SourceHanSansCN-Regular", "otf");
        geometos = new Font(nvgContext, "Geometos");
        montserrat = new Font(nvgContext, "Montserrat");
        montserratBold = new Font(nvgContext, "Montserrat-Regular","otf");
        sourceSerif_CHS = new Font(nvgContext, "SourceHanSerif-Regular","ttc");
        jetBrainsMono = new Font(nvgContext, "JetBrainsMonoNerdFontMono-Regular","ttf");
    }
}
