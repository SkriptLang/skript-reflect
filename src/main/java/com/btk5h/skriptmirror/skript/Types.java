package com.btk5h.skriptmirror.skript;

import ch.njol.skript.classes.*;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.registrations.Converters;
import ch.njol.yggdrasil.Fields;
import com.btk5h.skriptmirror.ArrayWrapper;
import com.btk5h.skriptmirror.JavaType;
import com.btk5h.skriptmirror.LibraryLoader;
import com.btk5h.skriptmirror.Null;
import org.bukkit.event.Event;

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
            return null;
          }

          @Override
          public boolean canParse(ParseContext context) {
            return false;
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
          public Fields serialize(JavaType cls) throws NotSerializableException {
            Fields f = new Fields();
            f.putObject("type", cls.getJavaClass().getName());
            return f;
          }

          @Override
          public void deserialize(JavaType o, Fields f) throws StreamCorruptedException,
              NotSerializableException {

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
        }));

    Converters.registerConverter(ClassInfo.class, JavaType.class,
        ((Converter<ClassInfo, JavaType>) c -> new JavaType(c.getC())));

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
          public Fields serialize(Null o) throws NotSerializableException {
            return new Fields();
          }

          @Override
          public void deserialize(Null o, Fields f) throws StreamCorruptedException,
              NotSerializableException {

          }

          @Override
          protected Null deserialize(Fields fields) throws StreamCorruptedException,
              NotSerializableException {
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
        })
    );

    Classes.registerClass(new ClassInfo<>(Changer.ChangeMode.class, "changemode")
        .parser(new Parser<Changer.ChangeMode>() {
          @Override
          public Changer.ChangeMode parse(String s, ParseContext context) {
            s = s.toUpperCase();
            if (s.startsWith("TO ")) {
              s = s.substring(3).trim();
              try {
                return Changer.ChangeMode.valueOf(s.replace(' ', '_'));
              } catch (IllegalArgumentException ex) {
                return null;
              }
            }
            return null;
          }

          @Override
          public String toString(Changer.ChangeMode o, int flags) {
            return "to " + o.name().toLowerCase().replace('_', ' ');
          }

          @Override
          public String toVariableNameString(Changer.ChangeMode o) {
            return "changemode:" + o.name();
          }

          @Override
          public String getVariableNamePattern() {
            return "changemode:.+";
          }
        })
        .serializer(new EnumSerializer<>(Changer.ChangeMode.class))
    );

    Classes.registerClass(new ClassInfo<>(ArrayWrapper.class, "array")
        .parser(new Parser<ArrayWrapper>() {
          @Override
          public ArrayWrapper parse(String s, ParseContext context) {
            return null;
          }

          @Override
          public boolean canParse(ParseContext context) {
            return false;
          }

          @Override
          public String toString(ArrayWrapper o, int flags) {
            return Arrays.stream(o.getArray())
                .map(Classes::toString)
                .collect(Collectors.joining(", "));
          }

          @Override
          public String toVariableNameString(ArrayWrapper o) {
            return Arrays.toString(o.getArray());
          }

          @Override
          public String getVariableNamePattern() {
            return ".+";
          }
        })
    );
  }
}
