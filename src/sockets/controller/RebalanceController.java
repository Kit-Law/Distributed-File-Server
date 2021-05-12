package sockets.controller;

import java.io.IOException;

public class RebalanceController implements Runnable
{
	private int rebalancePeriod;
	
	public RebalanceController(int rebalancePeriod)
	{
		this.rebalancePeriod = rebalancePeriod;
	}
	
	@Override
	public void run()
	{
		while(true)
		{
			try
			{
				Thread.sleep(rebalancePeriod);
				
				ControllerServer.pause();
				
				RebalancingControllerServer.handleRebalance();
				
				ControllerServer.resume();
			}
			catch (InterruptedException | IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
