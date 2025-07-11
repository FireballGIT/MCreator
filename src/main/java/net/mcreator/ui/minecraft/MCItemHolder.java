/*
 * MCreator (https://mcreator.net/)
 * Copyright (C) 2020 Pylo and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.mcreator.ui.minecraft;

import net.mcreator.element.parts.MItemBlock;
import net.mcreator.minecraft.MCItem;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.dialogs.MCItemSelectorDialog;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.validation.IValidable;
import net.mcreator.ui.validation.Validator;
import net.mcreator.util.image.EmptyIcon;
import net.mcreator.util.image.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class MCItemHolder extends JButton implements IValidable {

	private static final Color err = new Color(204, 166, 175);
	private static final Color warn = new Color(236, 238, 207);
	private static final Color bg = new Color(140, 140, 140);

	private String block = "";
	private final MCItemSelectorDialog bs;

	private boolean showValidation = true;
	private boolean removeButtonHover;
	private boolean openOnRightClick = true;

	private final List<ActionListener> listeners = new ArrayList<>();

	private final MCreator mcreator;

	public MCItemHolder(MCreator mcreator, MCItem.ListProvider blocksConsumer) {
		this(mcreator, blocksConsumer, false, false);
	}

	public MCItemHolder(MCreator mcreator, MCItem.ListProvider blocksConsumer, boolean supportTags) {
		this(mcreator, blocksConsumer, supportTags, false);
	}

	public MCItemHolder(MCreator mcreator, MCItem.ListProvider blocksConsumer, boolean supportTags,
			boolean hasPotions) {
		this.mcreator = mcreator;
		bs = new MCItemSelectorDialog(mcreator, blocksConsumer, supportTags, hasPotions);
		bs.setItemSelectedListener(e -> {
			MCItem bsa = bs.getSelectedMCItem();
			if (bsa != null) {
				setBlock(new MItemBlock(mcreator.getWorkspace(), bsa.getName()));
			}
		});
		initGUI();
	}

	public MCItemHolder disableRightClick() {
		this.openOnRightClick = false;
		return this;
	}

	public void addBlockSelectedListener(ActionListener al) {
		listeners.add(al);
	}

	public void setBlock(MItemBlock mItemBlock) {
		if (mItemBlock != null && mItemBlock.isValidReference()) {
			setIcon(new ImageIcon(ImageUtils.resizeAA(
					MCItem.getBlockIconBasedOnName(mcreator.getWorkspace(), mItemBlock.getUnmappedValue()).getImage(),
					25)));
			this.block = mItemBlock.getUnmappedValue();
			this.setToolTipText(mItemBlock.getMappedValue());
		} else {
			setIcon(new EmptyIcon(25, 25));
			block = "";
			this.setToolTipText(null);
		}
		listeners.forEach(listener -> listener.actionPerformed(new ActionEvent("", 0, "")));
		getValidationStatus();
	}

	public MItemBlock getBlock() {
		return new MItemBlock(mcreator.getWorkspace(), block);
	}

	/**
	 * @return true if selector has item defined (air doesn't count as an item)
	 */
	public boolean containsItem() {
		return block != null && !block.isEmpty() && !(block.equals("Blocks.AIR") || block.equals("Blocks.VOID_AIR")
				|| block.equals("Blocks.CAVE_AIR"));
	}

	/**
	 * @return true if selector has item defined (air also counts)
	 */
	public boolean containsItemOrAir() {
		return block != null && !block.isEmpty();
	}

	private void initGUI() {
		setMargin(new Insets(0, 0, 0, 0));
		setIcon(new EmptyIcon(25, 25));
		setCursor(new Cursor(Cursor.HAND_CURSOR));
		setContentAreaFilled(false);
		setFocusPainted(false);
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder());
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (isEnabled() && (e.getButton() != MouseEvent.BUTTON3 || openOnRightClick)) {
					if ((e.getButton() == MouseEvent.BUTTON2
							|| e.getX() > 1 && e.getX() < 11 && e.getY() < getHeight() - 1
							&& e.getY() > getHeight() - 11) && !block.isEmpty()) {
						setBlock(null);
					} else {
						bs.setVisible(true); // show block selector
					}
					repaint();
				}
			}

			@Override public void mouseExited(MouseEvent e) {
				removeButtonHover = false;
				repaint();
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override public void mouseMoved(MouseEvent e) {
				super.mouseMoved(e);
				removeButtonHover =
						e.getX() > 1 && e.getX() < 11 && e.getY() < getHeight() - 1 && e.getY() > getHeight() - 11;
				repaint();
			}
		});
	}

	@Override public void paintComponent(Graphics g) {

		if (showValidation && validator != null && currentValidationResult != null && (
				currentValidationResult.getValidationResultType() == Validator.ValidationResultType.ERROR
						|| currentValidationResult.getValidationResultType() == Validator.ValidationResultType.WARNING))
			g.setColor(currentValidationResult.getValidationResultType() == Validator.ValidationResultType.ERROR ?
					err :
					warn);
		else
			g.setColor(isEnabled() ? bg : bg.brighter());

		g.fillRect(0, 0, getWidth(), getHeight());

		super.paintComponent(g);

		if (showValidation) {
			if (!block.isEmpty()) {
				ImageIcon removeIcon;
				if (removeButtonHover || !isEnabled()) {
					removeIcon = ImageUtils.changeSaturation(UIRES.get("18px.remove"), 0.6f);
				} else {
					removeIcon = UIRES.get("18px.remove");
				}
				g.drawImage(removeIcon.getImage(), 0, getHeight() - 11, 11, 11, null);
			}

			if (validator != null && currentValidationResult != null) {
				if (currentValidationResult.getValidationResultType() == Validator.ValidationResultType.WARNING) {
					g.drawImage(UIRES.get("18px.warning").getImage(), getWidth() - 11, getHeight() - 11, 11, 11, null);
				} else if (currentValidationResult.getValidationResultType() == Validator.ValidationResultType.ERROR) {
					g.drawImage(UIRES.get("18px.remove").getImage(), getWidth() - 11, getHeight() - 11, 11, 11, null);
				}
			}

		}

	}

	public void setValidationShownFlag(boolean showValidation) {
		this.showValidation = showValidation;
		repaint();
	}

	//validation code
	private Validator validator = null;
	private Validator.ValidationResult currentValidationResult = null;

	@Override public Validator.ValidationResult getValidationStatus() {
		Validator.ValidationResult validationResult = validator == null ? null : validator.validateIfEnabled(this);

		this.currentValidationResult = validationResult;

		//repaint as new validation status might have to be rendered
		repaint();

		return validationResult;
	}

	@Override public void setValidator(Validator validator) {
		this.validator = validator;
	}

	@Override public Validator getValidator() {
		return validator;
	}
}