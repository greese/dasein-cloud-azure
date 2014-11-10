package org.dasein.cloud.azure.compute.vm;

import org.dasein.cloud.*;
import org.dasein.cloud.azure.Azure;
import org.dasein.cloud.compute.Architecture;
import org.dasein.cloud.compute.ImageClass;
import org.dasein.cloud.compute.Platform;
import org.dasein.cloud.compute.VirtualMachineCapabilities;
import org.dasein.cloud.compute.VMScalingCapabilities;
import org.dasein.cloud.compute.VmState;
import org.dasein.cloud.util.NamingConstraints;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Locale;

/**
 * Describes the capabilities of Azure with respect to Dasein virtual machine operations.
 * <p>Created by Danielle Mayne: 4/03/14 14:00 PM</p>
 *
 * @author Danielle Mayne
 * @version 2014.03 initial version
 * @since 2014.03
 */
public class VMCapabilities extends AbstractCapabilities<Azure> implements VirtualMachineCapabilities {
    public VMCapabilities( @Nonnull Azure provider ) {
        super(provider);
    }

    @Override
    public boolean canAlter( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean canClone( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canPause( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canReboot( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return true;
    }

    @Override
    public boolean canResume( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canStart( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return !fromState.equals(VmState.RUNNING);
    }

    @Override
    public boolean canStop( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return !fromState.equals(VmState.STOPPED);
    }

    @Override
    public boolean canSuspend( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean canTerminate( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return !fromState.equals(VmState.TERMINATED);
    }

    @Override
    public boolean canUnpause( @Nonnull VmState fromState ) throws CloudException, InternalException {
        return false;
    }

    @Override
    public int getMaximumVirtualMachineCount() throws CloudException, InternalException {
        return -2;
    }

    @Override
    public int getCostFactor( @Nonnull VmState state ) throws CloudException, InternalException {
        return !state.equals(VmState.TERMINATED) ? 100 : 0;
    }

    @Nonnull
    @Override
    public String getProviderTermForVirtualMachine( @Nonnull Locale locale ) throws CloudException, InternalException {
        return "virtual machine";
    }

    @Nullable
    @Override
    public VMScalingCapabilities getVerticalScalingCapabilities() throws CloudException, InternalException {
        return VMScalingCapabilities.getInstance(false, true, Requirement.REQUIRED, Requirement.NONE);
    }

    @Nonnull
    @Override
    public NamingConstraints getVirtualMachineNamingConstraints() throws CloudException, InternalException {
        return NamingConstraints.getStrictInstance(3, 15).constrainedBy(new char[]{'-'});
    }

    @Nullable
    @Override
    public VisibleScope getVirtualMachineVisibleScope() {
        return null;
    }

    @Nullable
    @Override
    public VisibleScope getVirtualMachineProductVisibleScope() {
        return null;
    }

    @Nonnull
    @Override
    public Requirement identifyDataCenterLaunchRequirement() throws CloudException, InternalException {
        return Requirement.REQUIRED;
    }

    @Nonnull
    @Override
    public Requirement identifyImageRequirement( @Nonnull ImageClass cls ) throws CloudException, InternalException {
        return ( cls.equals(ImageClass.MACHINE) ? Requirement.REQUIRED : Requirement.NONE );
    }

    @Nonnull
    @Override
    public Requirement identifyPasswordRequirement( Platform platform ) throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    @Nonnull
    @Override
    public Requirement identifyRootVolumeRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Nonnull
    @Override
    public Requirement identifyShellKeyRequirement( Platform platform ) throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    @Nonnull
    @Override
    public Requirement identifyStaticIPRequirement() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Nonnull
    @Override
    public Requirement identifySubnetRequirement() throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    @Nonnull
    @Override
    public Requirement identifyVlanRequirement() throws CloudException, InternalException {
        return Requirement.OPTIONAL;
    }

    @Override
    public boolean isAPITerminationPreventable() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isBasicAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isExtendedAnalyticsSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isUserDataSupported() throws CloudException, InternalException {
        return false;
    }

    @Override
    public boolean isUserDefinedPrivateIPSupported() throws CloudException, InternalException{return false;}

    @Nonnull
    @Override
    public Iterable<Architecture> listSupportedArchitectures() throws InternalException, CloudException {
        return Collections.singletonList(Architecture.I64);
    }

    @Override
    public boolean supportsSpotVirtualMachines() throws InternalException, CloudException {
        return false;
    }

    /**
     * Non VMState Defined lifecycle supported operations
     * The 'can' operations return similar values but based on a specific VM state. These return whether or not there is support at all.
     */
    @Override
    public boolean supportsAlterVM() {
        return true;
    }

    @Override
    public boolean supportsClone() {
        return false;
    }

    @Override
    public boolean supportsPause() {
        return false;
    }

    @Override
    public boolean supportsReboot() {
        return true;
    }

    @Override
    public boolean supportsResume() {
        return false;
    }

    @Override
    public boolean supportsStart() {
        return true;
    }

    @Override
    public boolean supportsStop() {
        return true;
    }

    @Override
    public boolean supportsSuspend() {
        return false;
    }

    @Override
    public boolean supportsTerminate() {
        return true;
    }

    @Override
    public boolean supportsUnPause() {
        return false;
    }
}
