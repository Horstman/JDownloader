package jd.plugins.optional.awesomebar.awesome.jlist;

import javax.swing.DefaultListSelectionModel;

public class AwesomeProposalListSelectionModel extends DefaultListSelectionModel {
	private static final long serialVersionUID = -6747860991499529046L;

	public AwesomeProposalListSelectionModel(){
		super();
		this.setSelectionMode(SINGLE_SELECTION);
	}
}