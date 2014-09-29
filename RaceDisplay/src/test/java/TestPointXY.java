import com.zuehlke.carrera.model.PointXY;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by wgiersche on 29.09.2014.
 */
public class TestPointXY {

    @Test
    public void testGeneral(){

        PointXY p = new PointXY(10, 20);

        Assert.assertEquals(p.x, 10f);
    }

}
