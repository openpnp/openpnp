
import org.junit.Assert;
import org.junit.Test;
import org.openpnp.model.eagle.EagleLoader;
import org.openpnp.model.eagle.xml.Board;
import org.openpnp.model.eagle.xml.Element;
import org.openpnp.model.eagle.xml.Instance;
import org.openpnp.model.eagle.xml.Schematic;



public class EagleLoaderTest {
    @Test
    public void testLoadBoard() throws Exception {
        EagleLoader loader =
                new EagleLoader(ClassLoader.getSystemResourceAsStream("samples/eagle/eagle.brd"));
        Board board = loader.board;
        Element r1 = board.getElements().getElement().get(0);
        Assert.assertEquals(r1.getName(), "R1");
    }

    @Test
    public void testLoadSchematic() throws Exception {
        EagleLoader loader =
                new EagleLoader(ClassLoader.getSystemResourceAsStream("samples/eagle/eagle.sch"));
        Schematic sch = loader.schematic;
        Instance r1 = sch.getSheets().getSheet().get(0).getInstances().getInstance().get(0);
        Assert.assertEquals(r1.getPart(), "R1");
    }
}
