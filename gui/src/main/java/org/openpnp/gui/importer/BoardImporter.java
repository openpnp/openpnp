package org.openpnp.gui.importer;

import org.openpnp.model.Board;

public interface BoardImporter {
	public Board importBoard() throws Exception;
}
