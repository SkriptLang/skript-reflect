package com.btk5h.skriptmirror.skript;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Converters;
import ch.njol.yggdrasil.Fields;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import com.btk5h.skriptmirror.Null;
import com.btk5h.skriptmirror.ObjectWrapper;
import com.btk5h.skriptmirror.skript.custom.CustomImport;
import com.btk5h.skriptmirror.skript.reflect.sections.Section;
import com.btk5h.skriptmirror.util.SkriptUtil;
import org.bukkit.event.Event;

import java.io.File;
import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Types {
  static {
    Classes.registerClass(new ClassInfo<>(Event.class, "event")
        .user("events?")
        .parser(new Parser<Event>() {
          @Override
          public Event parse(String s, ParseContext parseContext) {
            return null;
          }

          @Override
          public boolean canParse(ParseContext context) {
            return false;
          }

          @Override
          public String toString(Event e, int i) {
            return e.getEventName();
          }

          @Override
          public String toVariableNameString(Event e) {
            return e.toString();
          }

          @Override
          public String getVariableNamePattern() {
            return ".+";
          }
        }));

    Classes.registerClass(new ClassInfo<>(JavaType.class, "javatype")
        .user("javatypes?")
        .parser(new Parser<JavaType>() {
          @Override
          public JavaType parse(String s, ParseContext context) {
            File script = SkriptUtil.getCurrentScript();
            return CustomImport.lookup(script, s);
          }

          @Override
          public boolean canParse(ParseContext context) {
            // default context handled in CustomImport$ImportHandler
            return context != ParseContext.DEFAULT;
          }

          @Override
          public String toString(JavaType o, int flags) {
            return o.getJavaClass().getName();
          }

          @Override
          public String toVariableNameString(JavaType o) {
            return "type:" + o.getJavaClass().getName();
          }

          @Override
          public String getVariableNamePattern() {
            return "type:.+";
          }
        })
        .serializer(new Serializer<JavaType>() {
          @Override
          public Fields serialize(JavaType cls) {
            Fields f = new Fields();
            f.putObject("type", cls.getJavaClass().getName());
            return f;
          }

          @Override
          public void deserialize(JavaType o, Fields f) {

          }

          @Override
          protected JavaType deserialize(Fields fields) throws StreamCorruptedException,
              NotSerializableException {
            try {
              return new JavaType(LibraryLoader.getClassLoader().loadClass((String) fields.getObject("type")));
            } catch (ClassNotFoundException e) {
              throw new NotSerializableException();
            }
          }

          @Override
          public boolean mustSyncDeserialization() {
            return false;
          }

          @Override
          public boolean canBeInstantiated(Class<? extends JavaType> aClass) {
            return false;
          }

          @Override
          protected boolean canBeInstantiated() {
            return false;
          }
        }));

    Converters.registerConverter(ClassInfo.class, JavaType.class, c -> new JavaType(c.getC()));

    Classes.registerClass(new ClassInfo<>(Null.class, "null")
        .parser(new Parser<Null>() {
          @Override
          public Null parse(String s, ParseContext context) {
            return null;
          }

          @Override
          public boolean canParse(ParseContext context) {
            return false;
          }

          @Override
          public String toString(Null o, int flags) {
            return "null";
          }

          @Override
          public String toVariableNameString(Null o) {
            return "null";
          }

          @Override
          public String getVariableNamePattern() {
            return "null";
          }
        })
        .serializer(new Serializer<Null>() {
          @Override
          public Fields serialize(Null o) {
            return new Fields();
          }

          @Override
          public void deserialize(Null o, Fields f) {

          }

          @Override
          protected Null deserialize(Fields fields) {
            return Null.getInstance();
          }

          @Override
          public boolean mustSyncDeserialization() {
            return false;
          }

          @Override
          public boolean canBeInstantiated(Class<? extends Null> c) {
            return false;
          }

          @Override
          protected boolean canBeInstantiated() {
            return false;
          }
        })
    );

    Classes.registerClass(new ClassInfo<>(ObjectWrapper.class, "javaobject")
        .user("javaobjects?")
        .parser(new Parser<ObjectWrapper>() {
          @Override
          public ObjectWrapper parse(String s, ParseContext context) {
            return null;
          }

          @Override
          public boolean canParse(ParseContext context) {
            return false;
          }

          @Override
          public String toString(ObjectWrapper o, int flags) {
            if (o instanceof ObjectWrapper.OfArray) {
              return Arrays.stream(((ObjectWrapper.OfArray) o).get())
                  .map(Classes::toString)
                  .collect(Collectors.joining(", "));
            }

            return Classes.toString(o.get());
          }

          @Override
          public String toVariableNameString(ObjectWrapper o) {
            return o.toString();
          }

          @Override
          public String getVariableNamePattern() {
            return ".+";
          }
        })
    );

    Classes.registerClass(new ClassInfo<>(Section.class, "section")
      .user("sections?")
    );
  }
}
