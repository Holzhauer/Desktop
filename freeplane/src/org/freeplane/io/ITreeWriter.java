/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.io;

import java.io.IOException;

import org.freeplane.io.xml.n3.nanoxml.IXMLElement;

public interface ITreeWriter {
	void addAttribute(String name, double value);

	void addAttribute(String name, int value);

	void addAttribute(String name, String value);

	void addNode(Object userObject, IXMLElement element) throws IOException;

	void addNode(Object userObject, String name) throws IOException;

	void addNodeContent(String content) throws IOException;

	void addComment(String string) throws IOException;
}
