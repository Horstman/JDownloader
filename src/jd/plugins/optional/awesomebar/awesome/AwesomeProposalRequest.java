package jd.plugins.optional.awesomebar.awesome;

import java.util.EventObject;


public class AwesomeProposalRequest extends EventObject
{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4142238983507559780L;
	
	private final String command;
	private final String params;

	public AwesomeProposalRequest (Awesome source, String command, String params)
	{
		super(source);
		this.command = command;
		this.params = params;
	}
	
	/**
	 * @return the event command
	 */
	public String getCommand() {
		return command;
	}
	/**
	 * @return the event prams
	 */
	public String getParams() {
		return params;
	}
	
	public Awesome getSource(){
		return (Awesome)super.getSource();
	}

}