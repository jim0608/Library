package com.mysafe.lib_identification;

import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.VersionInfo;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void getArcVersion(){
        VersionInfo versionInfo = new VersionInfo();
        int code = FaceEngine.getVersion(versionInfo);
        System.out.println(code);
    }
}