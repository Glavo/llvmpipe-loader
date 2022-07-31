package org.glavo.llvmpipe.loader;

import java.io.*;
import java.util.Locale;

public final class Loader {
    private static String readVersion() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(Loader.class.getResourceAsStream("version.txt")))) {
            return input.readLine();
        } catch (Throwable e) {
            return null;
        }
    }

    private enum Architecture {
        X86(false, "x86"),
        X86_64(true, "x86-64"),
        IA32(false, "IA-32"),
        IA64(true, "IA-64"),
        SPARC(false),
        SPARCV9(true, "SPARC V9"),
        ARM32(false),
        ARM64(true),
        MIPS(false),
        MIPS64(true),
        MIPSEL(false, "MIPSel"),
        MIPS64EL(true, "MIPS64el"),
        PPC(false, "PowerPC"),
        PPC64(true, "PowerPC-64"),
        PPCLE(false, "PowerPC (Little-Endian)"),
        PPC64LE(true, "PowerPC-64 (Little-Endian)"),
        S390(false),
        S390X(true, "S390x"),
        RISCV(true, "RISC-V"),
        LOONGARCH32(false, "LoongArch32"),
        LOONGARCH64(true, "LoongArch64");

        private final String checkedName;
        private final String displayName;
        private final boolean is64Bit;

        Architecture(boolean is64Bit) {
            this.checkedName = this.toString().toLowerCase(Locale.ROOT);
            this.displayName = this.toString();
            this.is64Bit = is64Bit;
        }

        Architecture(boolean is64Bit, String displayName) {
            this.checkedName = this.toString().toLowerCase(Locale.ROOT);
            this.displayName = displayName;
            this.is64Bit = is64Bit;
        }

        public boolean is64Bit() {
            return is64Bit;
        }

        public String getCheckedName() {
            return checkedName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isX86() {
            return this == X86 || this == X86_64;
        }

        public static final Architecture CURRENT_ARCH = parseArchName(System.getProperty("os.arch"));

        public static Architecture parseArchName(String value) {
            if (value == null) {
                return null;
            }
            value = value.trim().toLowerCase(Locale.ROOT);

            switch (value) {
                case "x8664":
                case "x86-64":
                case "x86_64":
                case "amd64":
                case "ia32e":
                case "em64t":
                case "x64":
                    return X86_64;
                case "x8632":
                case "x86-32":
                case "x86_32":
                case "x86":
                case "i86pc":
                case "i386":
                case "i486":
                case "i586":
                case "i686":
                case "ia32":
                case "x32":
                    return X86;
                case "arm64":
                case "aarch64":
                    return ARM64;
                case "arm":
                case "arm32":
                    return ARM32;
                case "mips64":
                    return MIPS64;
                case "mips64el":
                    return MIPS64EL;
                case "mips":
                case "mips32":
                    return MIPS;
                case "mipsel":
                case "mips32el":
                    return MIPSEL;
                case "riscv":
                case "risc-v":
                    return RISCV;
                case "ia64":
                case "ia64w":
                case "itanium64":
                    return IA64;
                case "ia64n":
                    return IA32;
                case "sparcv9":
                case "sparc64":
                    return SPARCV9;
                case "sparc":
                case "sparc32":
                    return SPARC;
                case "ppc64":
                case "powerpc64":
                    return "little".equals(System.getProperty("sun.cpu.endian")) ? PPC64LE : PPC64;
                case "ppc64le":
                case "powerpc64le":
                    return PPC64LE;
                case "ppc":
                case "ppc32":
                case "powerpc":
                case "powerpc32":
                    return PPC;
                case "ppcle":
                case "ppc32le":
                case "powerpcle":
                case "powerpc32le":
                    return PPCLE;
                case "s390":
                    return S390;
                case "s390x":
                    return S390X;
                case "loongarch32":
                    return LOONGARCH32;
                case "loongarch64":
                    return LOONGARCH64;
                default:
                    if (value.startsWith("armv7")) {
                        return ARM32;
                    }
                    if (value.startsWith("armv8") || value.startsWith("armv9")) {
                        return ARM64;
                    }
                    return null;
            }
        }
    }

    public static void premain(String ignored) {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
            System.err.println("[llvmpipe-loader] unsupported operating system: " + System.getProperty("os.name"));
            return;
        }

        if (Architecture.CURRENT_ARCH != Architecture.X86_64) {
            System.err.println("[llvmpipe-loader] unsupported architecture: " + Architecture.CURRENT_ARCH.getCheckedName());
            return;
        }

        String version = readVersion();
        String path = "windows-" + Architecture.CURRENT_ARCH.getCheckedName() + "/opengl32.dll";
        try (InputStream input = Loader.class.getResourceAsStream(path)) {
            assert input != null;

            File targetFile = new File(System.getProperty("java.io.tmpdir")
                    + "/org/glavo/llvmpipe/loader/"
                    + (version == null ? "" : version + "/")
                    + path).getAbsoluteFile();

            if (!targetFile.exists() || targetFile.length() != input.available()) {
                File targetDir = targetFile.getParentFile();
                System.out.println("[llvmpipe-loader] Extract llvmpipe to " + targetDir);
                targetDir.mkdirs();
                byte[] buffer = new byte[1024];
                try (FileOutputStream out = new FileOutputStream(targetFile)) {
                    int n;
                    while ((n = input.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                }
            }

            String dllPath = targetFile.getAbsolutePath();

            System.out.println("[llvmpipe-loader] Loading " + dllPath);
            System.load(dllPath);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load llvmpipe");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Failed to extract llvmpipe");
            e.printStackTrace();
        }
    }
}
