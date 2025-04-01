import com.sun.media.jai.codecimpl.util.RasterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoKeyEntry;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.geometry.Envelope2D;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.swing.JMapFrame;
import org.junit.Test;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;

import javax.media.jai.PlanarImage;
import javax.swing.*;
import java.awt.*;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

/**
 * @author zazhi
 * @date 2025/3/15
 * @description: TODO
 */
public class TifTest {
    String filePath = "E:\\遥感数据\\LE07_L1TP_129039_20210723_20210723_01\\LE07_L1TP_129039_20210723_20210723_01_RT_B1.TIF";
//    String filePath = "http://localhost:9090/browser/minio-upload-demo/2025-03-23%2F93c2c6cf-ac06-4eba-9371-0525dcf4b2c0.TIF";
    File tiffFile = new File(filePath);
    GeoTiffReader reader;
    {
        try {
            reader = new GeoTiffReader(tiffFile);
        } catch (DataSourceException e) {
            throw new RuntimeException("读取文件失败");
        }
    }



    @Test
    public void readTifInfo() throws Exception {
        // GridEnvelope: 网格的结构
        GridEnvelope originalGridRange = reader.getOriginalGridRange();

        // dimension: 维度
        int dimension = originalGridRange.getDimension();

        // GridCoordinates: 包含各个维度的坐标信息
        GridCoordinates highCoordinates = originalGridRange.getHigh();
        int[] maxCoordinates = highCoordinates.getCoordinateValues(); // 横纵坐标的最大值：[x, y](二维)
        GridCoordinates minCoordinates = originalGridRange.getLow();
        int[] minCoordinateValues = minCoordinates.getCoordinateValues(); // 横纵坐标的最小值：[x, y](二维)
        int w = maxCoordinates[0] + 1; // 宽度
        int h = maxCoordinates[1] + 1; // 高度

        // 元数据
        GeoTiffIIOMetadataDecoder metadata = reader.getMetadata();
        // GeoKey是GeoTIFF中存储地理参考信息的键值对（例如坐标系、投影参数等）。
        Collection<GeoKeyEntry> geoKeys = metadata.getGeoKeys();
//        geoKeys.forEach(System.out::println);

        Format format = reader.getFormat();
        System.out.println(format.getDescription());
        System.out.println(format.getDocURL());
        System.out.println(format.getName());
        System.out.println(format.getVendor());
        System.out.println(format.getVersion());
    }
}
