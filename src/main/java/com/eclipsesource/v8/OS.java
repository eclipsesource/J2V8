package com.eclipsesource.v8;

public class OS {

    private static OS OS_INSTANCE = new OS(Type.getByCurrentOS(), Arch.getByCurrentArch());

    public static OS getCurrentOS() {
        return OS_INSTANCE;
    }

    private Type type;
    private Arch arch;

    public OS(Type type, Arch arch) {
        this.type = type;
        this.arch = arch;
    }

    public Type getType() {
        return type;
    }

    public Arch getArch() {
        return arch;
    }

    public enum Type {
        WINDOWS("Windows", "win", "", "dll"), LINUX("Linux", "linux", "lib", "so"), MAC("Mac", "mac", "lib", "dylib"), ANDROID("Android", "android", "lib", "so");

        private String name;
        private String shortName;
        private String sharedLibraryPrefix;
        private String sharedLibraryExtension;

        Type(String name, String shortName, String sharedLibraryPrefix, String sharedLibraryExtension) {
            this.name = name;
            this.shortName = shortName;
            this.sharedLibraryPrefix = sharedLibraryPrefix;
            this.sharedLibraryExtension = sharedLibraryExtension;
        }

        public String getName() {
            return name;
        }

        public String getShortName() {
            return shortName;
        }

        public String getSharedLibraryPrefix() {
            return sharedLibraryPrefix;
        }

        public String getSharedLibraryExtension() {
            return sharedLibraryExtension;
        }

        @Override
        public String toString() {
            return "Type{" +
                    "name='" + name + '\'' +
                    ", shortName='" + shortName + '\'' +
                    ", sharedLibraryPrefix='" + sharedLibraryPrefix + '\'' +
                    ", sharedLibraryExtension='" + sharedLibraryExtension + '\'' +
                    '}';
        }

        public static Type getByCurrentOS() {
            String osDesc = System.getProperty("os.name") + System.getProperty("java.specification.vendor");
            if (osDesc.startsWith("Windows")) {
                return WINDOWS;
            } else if (osDesc.startsWith("Mac")) {
                return MAC;
            } else if (osDesc.startsWith("Linux")) {
                return LINUX;
            } else if (osDesc.contains("Android")) {
                return ANDROID;
            } else {
                throw new RuntimeException("os \"" + osDesc + "\" is unknown");
            }
        }
    }

    public enum Arch {
        x86("x86"), x64("x64"), ARMv7("armv7"), ARMv8("armv8"), MIPS("mips"), MIPS64("mips64"), PPC("ppc");
        private String name;

        Arch(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Arch{" +
                    "name='" + name + '\'' +
                    '}';
        }

        public static Arch getByCurrentArch() {
            String archString = System.getProperty("os.arch");
            switch (archString) {
                case "x86":
                case "i686":
                case "i386":
                case "ia32":
                    return x86;
                case "x64":
                case "x86_64":
                case "amd64":
                    return x64;
                case "armeabi-v7a":
                case "armeabi":
                case "armv71":
                    return ARMv7;
                case "aarch64":
                case "armv8":
                case "arm64":
                    return ARMv8;
                case "mips":
                    return MIPS;
                case "mips64":
                    return MIPS64;
                case "ppc":
                    return PPC;
                default:
                    throw new RuntimeException("arch \"" + archString + "\" is unknown");
            }
        }

    }

    @Override
    public String toString() {
        return "OS{" +
                "type=" + type +
                ", arch=" + arch +
                '}';
    }

}
