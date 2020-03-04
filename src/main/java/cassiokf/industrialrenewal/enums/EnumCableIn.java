package cassiokf.industrialrenewal.enums;

import net.minecraft.util.math.MathHelper;

public enum EnumCableIn
{
    NONE(0),
    LV(1),
    MV(2),
    HV(3),
    UV(4);

    public static final EnumCableIn[] VALUES = values();
    private int index;

    EnumCableIn(int i)
    {
        index = i;
    }

    public static EnumCableIn byIndex(int i)
    {
        return VALUES[MathHelper.abs(i % VALUES.length)];
    }

    public int getIndex()
    {
        return ordinal();
    }
}
