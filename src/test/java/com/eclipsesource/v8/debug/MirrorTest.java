/*******************************************************************************
 * Copyright (c) 2016 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.v8.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.debug.DebugHandler.DebugEvent;
import com.eclipsesource.v8.debug.mirror.ArrayMirror;
import com.eclipsesource.v8.debug.mirror.BooleanMirror;
import com.eclipsesource.v8.debug.mirror.Frame;
import com.eclipsesource.v8.debug.mirror.NumberMirror;
import com.eclipsesource.v8.debug.mirror.ObjectMirror;
import com.eclipsesource.v8.debug.mirror.ObjectMirror.PropertyKind;
import com.eclipsesource.v8.debug.mirror.PropertiesArray;
import com.eclipsesource.v8.debug.mirror.PropertyMirror;
import com.eclipsesource.v8.debug.mirror.StringMirror;
import com.eclipsesource.v8.debug.mirror.ValueMirror;

public class MirrorTest {

    private static String script = "            // 1  \n"
            + "function foo(a, b, c)  {         // 2  \n"
            + "  var integer = 7;               // 3  \n"
            + "  var boolean = false;           // 4  \n"
            + "  var obj = { 'num' : 3,         // 5  \n"
            + "              'bool' : false,    // 6  \n"
            + "              'string' : 'bar',  // 7  \n"
            + "              'float' : 3.14 };  // 8  \n"
            + "  var array = [1,2,3];           // 9  \n"
            + "  var string = 'foo';            // 10 \n"
            + "  var nullValue = null;          // 11 \n"
            + "  var undef;                     // 12 \n"
            + "  var fun = function() {};       // 13 \n"
            + "  return obj;                    // 14 \n"
            + "}                                // 15 \n"
            + "foo(1,2,'yes').foo;              // 16 \n";

    private Object       result = false;;
    private V8           v8;
    private DebugHandler debugHandler;
    private BreakHandler breakHandler;

    @Before
    public void setup() {
        V8.setFlags("--expose-debug-as=" + DebugHandler.DEBUG_OBJECT_NAME);
        v8 = V8.createV8Runtime();
        debugHandler = new DebugHandler(v8);
        debugHandler.setScriptBreakpoint("script", 14);
        breakHandler = mock(BreakHandler.class);
        debugHandler.addBreakHandler(breakHandler);
    }

    @After
    public void tearDown() {
        try {
            debugHandler.release();
            v8.release();
            if (V8.getActiveRuntimes() != 0) {
                throw new IllegalStateException("V8Runtimes not properly released");
            }
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testEquals() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror objectValue1 = frame.getLocalValue(2);
                ValueMirror objectValue2 = frame.getLocalValue(2);
                result = objectValue1.equals(objectValue2);
                result = (Boolean) result & objectValue2.equals(objectValue1);
                objectValue1.release();
                objectValue2.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testHashEquals() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror objectValue1 = frame.getLocalValue(2);
                ValueMirror objectValue2 = frame.getLocalValue(2);
                result = objectValue1.hashCode() == objectValue2.hashCode();
                objectValue1.release();
                objectValue2.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testNotEquals() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror objectValue1 = frame.getLocalValue(2);
                ValueMirror objectValue2 = frame.getLocalValue(1);
                result = objectValue1.equals(objectValue2);
                objectValue1.release();
                objectValue2.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertFalse((Boolean) result);
    }

    @Test
    public void testNotEqualsWrongType() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror objectValue1 = frame.getLocalValue(2);
                result = objectValue1.equals(new Object());
                objectValue1.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertFalse((Boolean) result);
    }

    @Test
    public void testNotEqualsNull() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror objectValue1 = frame.getLocalValue(2);
                result = objectValue1.equals(null);
                objectValue1.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertFalse((Boolean) result);
    }

    @Test
    public void testGetNumberValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror number = frame.getLocalValue(0);
                result = number.isValue() && number.isNumber();
                result = (Boolean) result && number.getValue().equals(7);
                number.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetBooleanValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror booleanValue = frame.getLocalValue(1);
                result = booleanValue.isValue() && booleanValue.isBoolean();
                result = (Boolean) result && booleanValue.getValue().equals(false);
                booleanValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetObjectValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror objectValue = frame.getLocalValue(2);
                result = objectValue.isValue() && objectValue.isObject();
                objectValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetObjectProperties() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ObjectMirror objectValue = (ObjectMirror) frame.getLocalValue(2);
                PropertiesArray properties = objectValue.getProperties(PropertyKind.Named, 0);
                result = properties.length() == 4;
                PropertyMirror property = properties.getProperty(0);
                result = (Boolean) result && property.isProperty();
                result = (Boolean) result && property.getName().equals("num");
                properties.release();
                property.release();
                objectValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetObjectProperties_Number() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ObjectMirror objectValue = (ObjectMirror) frame.getLocalValue(2);
                PropertiesArray properties = objectValue.getProperties(PropertyKind.Named, 0);
                PropertyMirror property = properties.getProperty(0);
                result = property.getName().equals("num");
                NumberMirror value = (NumberMirror) property.getValue();
                result = (Boolean) result && value.isNumber();
                result = (Boolean) result && value.toString().equals("3");
                value.release();
                properties.release();
                property.release();
                objectValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetObjectProperties_Boolean() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ObjectMirror objectValue = (ObjectMirror) frame.getLocalValue(2);
                PropertiesArray properties = objectValue.getProperties(PropertyKind.Named, 0);
                PropertyMirror property = properties.getProperty(1);
                result = property.getName().equals("bool");
                BooleanMirror value = (BooleanMirror) property.getValue();
                result = (Boolean) result && value.isBoolean();
                result = (Boolean) result && value.toString().equals("false");
                value.release();
                properties.release();
                property.release();
                objectValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetObjectProperties_String() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ObjectMirror objectValue = (ObjectMirror) frame.getLocalValue(2);
                PropertiesArray properties = objectValue.getProperties(PropertyKind.Named, 0);
                PropertyMirror property = properties.getProperty(2);
                result = property.getName().equals("string");
                StringMirror value = (StringMirror) property.getValue();
                result = (Boolean) result && value.isString();
                result = (Boolean) result && value.toString().equals("bar");
                value.release();
                properties.release();
                property.release();
                objectValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetObjectProperties_Float() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ObjectMirror objectValue = (ObjectMirror) frame.getLocalValue(2);
                PropertiesArray properties = objectValue.getProperties(PropertyKind.Named, 0);
                PropertyMirror property = properties.getProperty(3);
                result = property.getName().equals("float");
                NumberMirror value = (NumberMirror) property.getValue();
                result = (Boolean) result && value.isNumber();
                result = (Boolean) result && value.toString().equals("3.14");
                value.release();
                properties.release();
                property.release();
                objectValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetArrayValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror arrayValue = frame.getLocalValue(3);
                result = arrayValue.isValue() && arrayValue.isObject() && arrayValue.isArray();
                result = (Boolean) result && (((ArrayMirror) arrayValue).length() == 3);
                arrayValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetStringValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror stringValue = frame.getLocalValue(4);
                result = stringValue.isValue() && stringValue.isString();
                result = (Boolean) result && stringValue.getValue().equals("foo");
                stringValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetNullValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror nullValue = frame.getLocalValue(5);
                result = nullValue.isValue() && nullValue.isNull();
                nullValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetUndefinedValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror undefinedValue = frame.getLocalValue(6);
                result = undefinedValue.isValue() && undefinedValue.isUndefined();
                undefinedValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testGetFunctionValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ValueMirror functionValue = frame.getLocalValue(7);
                result = functionValue.isValue() && functionValue.isFunction();
                functionValue.release();
                frame.release();
            }
        });

        v8.executeScript(script, "script", 0);

        assertTrue((Boolean) result);
    }

    @Test
    public void testChangeValue() {
        handleBreak(new BreakHandler() {

            @Override
            public void onBreak(final DebugEvent event, final ExecutionState state, final EventData eventData, final V8Object data) {
                Frame frame = state.getFrame(0);
                ObjectMirror mirror = (ObjectMirror) frame.getLocalValue(2);
                V8Object object = (V8Object) mirror.getValue();
                object.add("foo", 7);
                mirror.release();
                object.release();
                frame.release();
            }
        });

        int result = v8.executeIntegerScript(script, "script", 0);

        assertEquals(7, result);
    }

    private void handleBreak(final BreakHandler handler) {
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                DebugEvent arg1 = (DebugEvent) invocation.getArguments()[0];
                ExecutionState arg2 = (ExecutionState) invocation.getArguments()[1];
                EventData arg3 = (EventData) invocation.getArguments()[2];
                V8Object arg4 = (V8Object) invocation.getArguments()[3];
                handler.onBreak(arg1, arg2, arg3, arg4);
                return null;
            }

        }).when(breakHandler).onBreak(eq(DebugEvent.Break), any(ExecutionState.class), any(EventData.class), any(V8Object.class));
    }

}
