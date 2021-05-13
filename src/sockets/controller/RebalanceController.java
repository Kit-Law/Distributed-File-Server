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
				
				RebalancingControllerServer.handleRebalance();
			}
			catch (InterruptedException | IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}