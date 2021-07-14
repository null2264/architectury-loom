/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.configuration.providers.mappings.parchment;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;

public final class ParchmentTreeV1 {
	private final String version;
	private final List<Class> classes;
	private final List<Package> packages;

	ParchmentTreeV1(String version, List<Class> classes, List<Package> packages) {
		this.version = version;
		this.classes = classes;
		this.packages = packages;
	}

	public String version() {
		return version;
	}

	public List<Class> classes() {
		return classes;
	}

	public List<Package> packages() {
		return packages;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		ParchmentTreeV1 that = (ParchmentTreeV1) obj;
		return Objects.equals(this.version, that.version)
				&& Objects.equals(this.classes, that.classes)
				&& Objects.equals(this.packages, that.packages);
	}

	@Override
	public int hashCode() {
		return Objects.hash(version, classes, packages);
	}

	@Override
	public String toString() {
		return "ParchmentTreeV1["
				+ "version=" + version + ", "
				+ "classes=" + classes + ", "
				+ "packages=" + packages + ']';
	}

	public void visit(MappingVisitor visitor, String srcNamespace) {
		while (true) {
			if (visitor.visitHeader()) {
				visitor.visitNamespaces(srcNamespace, Collections.emptyList());
			}

			if (visitor.visitContent()) {
				if (classes() != null) {
					for (Class c : classes()) {
						c.visit(visitor);
					}
				}
			}

			if (visitor.visitEnd()) {
				break;
			}
		}
	}

	public static class Class {
		private final String name;
		@Nullable
		private final List<Field> fields;
		@Nullable
		private final List<Method> methods;
		@Nullable
		private final List<String> javadoc;

		public Class(String name, @Nullable List<Field> fields, @Nullable List<Method> methods, @Nullable List<String> javadoc) {
			this.name = name;
			this.fields = fields;
			this.methods = methods;
			this.javadoc = javadoc;
		}

		public String name() {
			return name;
		}

		public List<Field> fields() {
			return fields;
		}

		public List<Method> methods() {
			return methods;
		}

		public List<String> javadoc() {
			return javadoc;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Class)) return false;
			Class aClass = (Class) o;
			return Objects.equals(name, aClass.name) && Objects.equals(fields, aClass.fields) && Objects.equals(methods, aClass.methods) && Objects.equals(javadoc, aClass.javadoc);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, fields, methods, javadoc);
		}

		public void visit(MappingVisitor visitor) {
			if (visitor.visitClass(name())) {
				if (!visitor.visitElementContent(MappedElementKind.CLASS)) {
					return;
				}

				if (fields() != null) {
					for (Field field : fields()) {
						field.visit(visitor);
					}
				}

				if (methods() != null) {
					for (Method method : methods()) {
						method.visit(visitor);
					}
				}

				if (javadoc() != null) {
					visitor.visitComment(MappedElementKind.CLASS, String.join("\n", javadoc()));
				}
			}
		}
	}

	public static class Field {
		private final String name;
		private final String descriptor;
		@Nullable
		private final List<String> javadoc;

		public Field(String name, String descriptor, @Nullable List<String> javadoc) {
			this.name = name;
			this.descriptor = descriptor;
			this.javadoc = javadoc;
		}

		public String name() {
			return name;
		}

		public String descriptor() {
			return descriptor;
		}

		public List<String> javadoc() {
			return javadoc;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Field)) return false;
			Field field = (Field) o;
			return Objects.equals(name, field.name) && Objects.equals(descriptor, field.descriptor) && Objects.equals(javadoc, field.javadoc);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, descriptor, javadoc);
		}

		public void visit(MappingVisitor visitor) {
			if (visitor.visitField(name, descriptor)) {
				if (!visitor.visitElementContent(MappedElementKind.FIELD)) {
					return;
				}

				if (javadoc() != null) {
					visitor.visitComment(MappedElementKind.FIELD, String.join("\n", javadoc()));
				}
			}
		}
	}

	public static class Method {
		private final String name;
		private final String descriptor;
		@Nullable
		private final List<Parameter> parameters;
		@Nullable
		private final List<String> javadoc;

		public Method(String name, String descriptor, @Nullable List<Parameter> parameters, @Nullable List<String> javadoc) {
			this.name = name;
			this.descriptor = descriptor;
			this.parameters = parameters;
			this.javadoc = javadoc;
		}

		public String name() {
			return name;
		}

		public String descriptor() {
			return descriptor;
		}

		public List<Parameter> parameters() {
			return parameters;
		}

		public List<String> javadoc() {
			return javadoc;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Method)) return false;
			Method method = (Method) o;
			return Objects.equals(name, method.name) && Objects.equals(descriptor, method.descriptor) && Objects.equals(parameters, method.parameters) && Objects.equals(javadoc, method.javadoc);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, descriptor, parameters, javadoc);
		}

		public void visit(MappingVisitor visitor) {
			if (visitor.visitMethod(name, descriptor)) {
				if (!visitor.visitElementContent(MappedElementKind.METHOD)) {
					return;
				}

				if (parameters() != null) {
					for (Parameter parameter : parameters()) {
						parameter.visit(visitor);
					}
				}

				if (javadoc() != null) {
					visitor.visitComment(MappedElementKind.METHOD, String.join("\n", javadoc()));
				}
			}
		}
	}

	public static class Parameter {
		private final int index;
		private final String name;
		@Nullable
		private final String javadoc;

		public Parameter(int index, String name, @Nullable String javadoc) {
			this.index = index;
			this.name = name;
			this.javadoc = javadoc;
		}

		public int index() {
			return index;
		}

		public String name() {
			return name;
		}

		public String javadoc() {
			return javadoc;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Parameter)) return false;
			Parameter parameter = (Parameter) o;
			return index == parameter.index && Objects.equals(name, parameter.name) && Objects.equals(javadoc, parameter.javadoc);
		}

		@Override
		public int hashCode() {
			return Objects.hash(index, name, javadoc);
		}

		public void visit(MappingVisitor visitor) {
			if (visitor.visitMethodArg(index, index, name)) {
				if (!visitor.visitElementContent(MappedElementKind.METHOD_ARG)) {
					return;
				}

				if (javadoc() != null) {
					visitor.visitComment(MappedElementKind.METHOD_ARG, javadoc);
				}
			}
		}
	}

	public static class Package {
		private final String name;
		private final List<String> javadoc;

		public Package(String name, List<String> javadoc) {
			this.name = name;
			this.javadoc = javadoc;
		}

		public String name() {
			return name;
		}

		public List<String> javadoc() {
			return javadoc;
		}

		@Override
		public String toString() {
			return "Package{"
					+ "name='" + name + '\''
					+ ", javadoc=" + javadoc
					+ '}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof Package)) return false;
			Package aPackage = (Package) o;
			return Objects.equals(name, aPackage.name) && Objects.equals(javadoc, aPackage.javadoc);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, javadoc);
		}
	}
}
