package com.zazhi.demo;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoKeyEntry;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;

/**
 * @author zazhi
 * @date 2025/4/6
 * @description: TODO
 */
public class GeoTiffMetadata {

    public static void main(String[] args) throws Exception {
        String filePath = "E:\\RS_DATA\\LC81290392021036LGN00.tar\\LC08_L1TP_129039_20210205_20210304_01_T1_B5.TIF";
        // 1. 读取GeoTiff文件
        GeoTiffReader reader = readGeoTiff(filePath);
        // 2. 获取地理范围
        getEnvelope(reader);
        // 3. 获取网格范围
        getRange(reader);
        // 4. 获取空间分辨率
        getResolution(reader);
        // 5. 获取坐标参考系
        getCRS(reader);
        // 6. 获取波段数
        boudInfo(reader);

    }

    /**
     * 读取GeoTiff文件
     *
     * 这个类负责向 GeoTools 库提供图像数据以及地理参考元数据。
     * 该读取器在很大程度上依赖于 ImageIO 工具和 JAI（Java Advanced Imaging）库所提供的功能。
     *
     * 简单来说，它是 GeoTools 中专门用来读取 GeoTIFF 文件的类，会自动识别图像内容和坐标信息，
     * 底层依赖 Java 的图像处理和元数据解析工具包（ImageIO 和 JAI）。
     *
     * @param filePath 文件路径
     * @return
     * @throws Exception
     */
    public static GeoTiffReader readGeoTiff(String filePath) throws Exception{
        File file = new File(filePath);
        GeoTiffReader reader = new GeoTiffReader(file);
        return reader;
    }

    /**
     * 获取地理范围
     *
     * @param reader GeoTiffReader
     * @throws Exception
     */
    public static void getEnvelope(GeoTiffReader reader) throws Exception {
        GeneralEnvelope bounds = reader.getOriginalEnvelope();
        // 获取地理范围（跨度）；参数0/1分别表示x/y轴
        double xSpan = bounds.getSpan(0);
        double ySpan = bounds.getSpan(1);
        // 获取边界坐标
        double xMin = bounds.getMinimum(0);
        double yMin = bounds.getMinimum(1);
        double xMax = bounds.getMaximum(0);
        double yMax = bounds.getMaximum(1);
        System.out.println("xSpan = " + xSpan + ", ySpan = " + ySpan);
        System.out.println("xMin = " + xMin + ", yMin = " + yMin);
        System.out.println("xMax = " + xMax + ", yMax = " + yMax);
    }

    /**
     * 获取网格图像范围, 即图像的宽高
     *
     * @param reader
     * @throws Exception
     */
    public static void getRange(GeoTiffReader reader) throws Exception {
        // 获取网格范围
        GridEnvelope range = reader.getOriginalGridRange();
        int width = range.getSpan(0);
        int height = range.getSpan(1);
        System.out.println("width = " + width);
        System.out.println("height = " + height);
    }

    /**
     * 获取空间分辨率（像元大小）
     * 计算思路：用地理范围的跨度除以网格范围的跨度，即计算出每个网格代表的地理范围，即分辨率。
     *
     * @param reader
     * @throws Exception
     */
    public static void getResolution(GeoTiffReader reader) throws Exception {
        GeneralEnvelope bounds = reader.getOriginalEnvelope();
        GridEnvelope range = reader.getOriginalGridRange();
        double resX = bounds.getSpan(0) / range.getSpan(0);
        double resY = bounds.getSpan(1) / range.getSpan(1);
        System.out.println("Resolution: " + resX + " x " + resY);
    }

    /**
     * 获取坐标参考系（CRS）
     *
     * EPSG:4326 = WGS84 地理坐标系（经纬度）
     * EPSG:3857 = Web Mercator（在线地图）
     * EPSG:32648 = WGS84 UTM 投影第 48N 带
     * EPSG:4490 = CGCS2000 地理坐标系（国产 WGS84）
     * 你可以据此判断数据是否需要「投影转换」，或者是否能直接叠加在高德/天地图上。
     *
     * @param reader
     * @throws Exception
     */
    public static void getCRS(GeoTiffReader reader) throws Exception{
        // 获取投影信息（CRS）
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
        System.out.println("CRS Name: " + crs.getName().getCode());
        // 判断是否是投影坐标系
        boolean isProjected = CRS.getProjectedCRS(crs) != null;
        System.out.println("Is Projected: " + isProjected);
        Integer epsg = CRS.lookupEpsgCode(crs, true);
        System.out.println("EPSG Code: " + (epsg != null ? epsg : "未知或无效"));
    }

    /**
     * 获取波段数
     *
     * @param reader
     * @throws Exception
     */
    public static void boudInfo(GeoTiffReader reader) throws Exception {
        GridCoverage2D gridCoverage2D = reader.read(null);
        RenderedImage renderedImage = gridCoverage2D.getRenderedImage();
        SampleModel sampleModel = renderedImage.getSampleModel();
        int numBands = sampleModel.getNumBands();
        System.out.println("numBands = " + numBands);
    }

    /**
     * 获取像元数据类型
     * @param reader
     * @return
     * @throws Exception
     */
    public static String getDataType(GeoTiffReader reader) throws Exception {
        GridCoverage2D gridCoverage2D = reader.read(null);
        RenderedImage renderedImage = gridCoverage2D.getRenderedImage();
        SampleModel sampleModel = renderedImage.getSampleModel();
        int dataType = sampleModel.getDataType();
        String typeStr;
        switch (dataType) {
            case java.awt.image.DataBuffer.TYPE_BYTE:
                typeStr = "Byte (8-bit)";
                break;
            case java.awt.image.DataBuffer.TYPE_USHORT:
                typeStr = "Unsigned Short (16-bit)";
                break;
            case java.awt.image.DataBuffer.TYPE_SHORT:
                typeStr = "Signed Short (16-bit)";
                break;
            case java.awt.image.DataBuffer.TYPE_INT:
                typeStr = "Integer (32-bit)";
                break;
            case java.awt.image.DataBuffer.TYPE_FLOAT:
                typeStr = "Float32 (单精度浮点)";
                break;
            case java.awt.image.DataBuffer.TYPE_DOUBLE:
                typeStr = "Float64 (双精度浮点)";
                break;
            default:
                typeStr = "未知类型";
        }
        System.out.println("像元数据类型: " + typeStr);
        return typeStr;
    }

    public static void getColor(GeoTiffReader reader) throws Exception {
        GridCoverage2D gridCoverage2D = reader.read(null);
        RenderedImage renderedImage = gridCoverage2D.getRenderedImage();
        ColorModel colorModel = renderedImage.getColorModel();
        if (colorModel instanceof IndexColorModel) {
            IndexColorModel icm = (IndexColorModel) colorModel;
            int mapSize = icm.getMapSize();

            for (int i = 0; i < mapSize; i++) {
                int r = icm.getRed(i);
                int g = icm.getGreen(i);
                int b = icm.getBlue(i);
                System.out.println("Pixel value " + i + " -> RGB(" + r + "," + g + "," + b + ")");
            }
        } else {
            System.out.println("This image does not use a color palette.");
        }
    }

}
