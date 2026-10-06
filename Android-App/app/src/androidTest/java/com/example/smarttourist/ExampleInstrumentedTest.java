package com.example.smarttourist;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest { @Test public void useAppContext() { assertEquals("com.example.smarttourist", androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName()); } }