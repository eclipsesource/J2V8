
require 'vagrant'

# Define the plugin.
class SwitchToWinRMPlugin < Vagrant.plugin('2')
  name 'Switch to WinRM Plugin'
 
  # This plugin provides a provisioner called switch_to_winrm.
  provisioner 'switch_to_winrm' do
 
    # Create a provisioner.
    class SwitchToWinRMProvisioner < Vagrant.plugin('2', :provisioner)
      # Initialization, define internal state. Nothing needed.
      def initialize(machine, config)
        super(machine, config)
      end
 
      # Configuration changes to be done. Nothing needed here either.
      def configure(root_config)
        super(root_config)
      end
 
      # Run the provisioning.
      def provision
        _provisioned = @machine.config.vm.communicator == :winrm

        env = @machine.instance_variable_get(:@env)

        if not _provisioned
          # stop the VM before we switch the communicator
          puts "action_halt..."
          env.action_runner.run(VagrantPlugins::ProviderVirtualBox::Action::action_halt(), {
            machine: @machine,
            ui: @machine.ui,
          })

          # TODO: this is just a copy-paste of the settings from the actual Vagrantfile config
          # there should be some practical way to remove this code duplication!
          puts "switching comm..."
          @machine.config.vm.communicator = :winrm
          @machine.config.winrm.username = "IEUser"
          @machine.config.winrm.password = "Passw0rd!"
          @machine.config.winrm.timeout = 50000
          @machine.config.winrm.retry_delay = 30

          @machine.config.vm.synced_folder ".", "/vagrant", disabled: true
          @machine.config.vm.synced_folder "../../", "C:/j2v8", type: "virtualbox", smb_username: ENV['VAGRANT_SMB_USER'], smb_password: ENV['VAGRANT_SMB_PASSWORD']

          # NOTE: this is copied from https://github.com/mitchellh/vagrant/blob/d1a589c59f75dd2910e47976a742dc6bc99035b0/lib/vagrant/machine.rb#L246
          # it reinstantiates the communicator defined by the vagrant configuration ...
          requested  = @machine.config.vm.communicator
          requested ||= :ssh
          klass = Vagrant.plugin("2").manager.communicators[requested]
          raise Errors::CommunicatorNotFound, comm: requested.to_s if !klass

          comm = klass.new(@machine)

          # ... and then monkey-patches the new instance into the machine
          @machine.instance_variable_set(:@communicator, comm)
          puts "patched communicator"

          # this applies the changed communicator and also reconfigures the related network settings
          @machine.config.finalize!

          # start the VM now, after we successfully switched the communicator
          puts "action_boot..."
          env.action_runner.run(VagrantPlugins::ProviderVirtualBox::Action::action_boot(), {
            machine: @machine,
            ui: @machine.ui,
          })
        end
      end
 
      # Nothing needs to be done on cleanup.
      def cleanup
        super
      end
    end
    SwitchToWinRMProvisioner
 
  end
end
