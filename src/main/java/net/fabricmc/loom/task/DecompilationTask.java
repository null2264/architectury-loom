package net.fabricmc.loom.task;

import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;

// TODO: This should probably be replaced in favour of just using upstream Loom's decompiler options
@Deprecated
public interface DecompilationTask extends Task {
	RegularFileProperty getInputJar();
	RegularFileProperty getRuntimeJar();
}
