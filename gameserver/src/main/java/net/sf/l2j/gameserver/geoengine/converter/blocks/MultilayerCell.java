package net.sf.l2j.gameserver.geoengine.converter.blocks;

/**
 * @author Hasha
 */
public class MultilayerCell
{
	private final byte[] _nswe;
	private final short[] _height;
	
	public MultilayerCell(int layers)
	{
		_nswe = new byte[layers];
		_height = new short[layers];
	}
	
	public void setData(int layer, byte nswe, short height)
	{
		_nswe[layer] = nswe;
		_height[layer] = height;
	}
	
	public int getLayers()
	{
		return _nswe.length;
	}
	
	public byte getNSWE(int layer)
	{
		return _nswe[layer];
	}
	
	public short getHeight(int layer)
	{
		return _height[layer];
	}
	
	public void sort()
	{
		boolean sorted;
		do
		{
			sorted = true;
			
			for (int i = 0; i < _height.length - 1; i++)
			{
				if (_height[i] <= _height[i + 1])
					continue;
				
				short height = _height[i];
				_height[i] = _height[i + 1];
				_height[i + 1] = height;
				
				byte nswe = _nswe[i];
				_nswe[i] = _nswe[i + 1];
				_nswe[i + 1] = nswe;
				
				sorted = false;
			}
		}
		while (!sorted);
	}
	
	public void updateNswe(int layer, byte nswe)
	{
		_nswe[layer] = nswe;
	}
}