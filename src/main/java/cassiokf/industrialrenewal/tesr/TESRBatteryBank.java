package cassiokf.industrialrenewal.tesr;

import cassiokf.industrialrenewal.init.ModItems;
import cassiokf.industrialrenewal.tileentity.TileEntityBatteryBank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class TESRBatteryBank extends TileEntitySpecialRenderer<TileEntityBatteryBank> {

    private static ItemStack pointer = new ItemStack(ModItems.barLevel);
    private double xPos = 0D;
    private double zPos = 0D;

    @Override
    public void render(TileEntityBatteryBank te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        doTheMath(te.getBlockFacing(), x, z);
        RenderEnergyQuantity(te, xPos, y + 0.43, zPos);
        RenderPointer(te, xPos, y + 0.49, zPos);
    }

    private void doTheMath(EnumFacing facing, double x, double z) {
        switch (facing) {
            case SOUTH:
                xPos = x + 0.5;
                zPos = z - 0.023;
                return;
            case NORTH:
                xPos = x + 0.5;
                zPos = z + 1.023;
                return;
            case EAST:
                xPos = x - 0.023;
                zPos = z + 0.5;
                return;
            case WEST:
                xPos = x + 1.023;
                zPos = z + 0.5;
                return;
        }
    }

    /**
     * x = side / y = up / z = front
     */
    private void RenderEnergyQuantity(TileEntityBatteryBank te, double x, double y, double z) {

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        RenderHelper.disableStandardItemLighting();
        switch (te.getBlockFacing()) {
            default:
                System.out.println("DEU BOSTA AKI TIO: " + te.getBlockFacing());
                break;
            case SOUTH:
                GlStateManager.rotate(180F, 0, 1, 0);
                break;
            case NORTH:
                break;
            case WEST:
                GlStateManager.rotate(90F, 0, 1, 0);
                break;
            case EAST:
                GlStateManager.rotate(-90F, 0, 1, 0);
                break;
        }
        GlStateManager.rotate(180, 1, 0, 0);
        GlStateManager.scale(0.005F, 0.005F, 1F);
        String st = te.GetText();
        int xh = -Minecraft.getMinecraft().fontRenderer.getStringWidth(st) / 2;
        Minecraft.getMinecraft().fontRenderer.drawString(st, xh, 0, 0xFFFFFFFF);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void RenderPointer(TileEntityBatteryBank te, double x, double y, double z)
    {

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        switch (te.getBlockFacing())
        {
            default:
                break;
            case SOUTH:
                GlStateManager.rotate(180F, 0, 1, 0);
                break;
            case NORTH:
                break;
            case WEST:
                GlStateManager.rotate(90F, 0, 1, 0);
                break;
            case EAST:
                GlStateManager.rotate(-90F, 0, 1, 0);
                break;
        }
        float angle = te.GetTankFill();
        GlStateManager.scale(0.7F, angle * 0.7F, 0.05F);
        //GlStateManager.rotate(-angle, 0, 0, 1);
        Minecraft.getMinecraft().getRenderItem().renderItem(pointer, ItemCameraTransforms.TransformType.GUI);
        GlStateManager.popMatrix();
    }
}
