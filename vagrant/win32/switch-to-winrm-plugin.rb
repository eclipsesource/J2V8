
require 'vagrant'

# Define the plugin.
class SwitchToWinRMPlugin < Vagrant.plugin('2')
  name 'Switch to WinRM Plugin'
 
  # This plugin provides a provisioner called windows_reboot.
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
          puts "action_halt..."
          env.action_runner.run(VagrantPlugins::ProviderVirtualBox::Action::action_halt(), {
            machine: @machine,
            ui: @machine.ui,
          })
        # end

          puts "switching comm..."
          @machine.config.vm.communicator = :winrm
          @machine.config.winrm.username = "IEUser"
          @machine.config.winrm.password = "Passw0rd!"
          @machine.config.winrm.timeout = 50000
          @machine.config.winrm.retry_delay = 30

          @machine.config.vm.synced_folder ".", "/vagrant", disabled: true
          @machine.config.vm.synced_folder "../../", "C:/j2v8", type: "virtualbox", smb_username: ENV['VAGRANT_SMB_USER'], smb_password: ENV['VAGRANT_SMB_PASSWORD']

          requested  = @machine.config.vm.communicator
          requested ||= :ssh
          klass = Vagrant.plugin("2").manager.communicators[requested]
          raise Errors::CommunicatorNotFound, comm: requested.to_s if !klass

          # puts "inspect: ", Vagrant.plugin("2").manager.inspect  # Object
          # puts "instance_variables: ", Vagrant.plugin("2").manager.instance_variables  # Object
          # puts "methods:", Vagrant.plugin("2").manager.methods  # Object

          comm = klass.new(@machine)
          @machine.instance_variable_set(:@communicator, comm)
          puts "patched communicator"

          @machine.config.finalize!

        # if not _provisioned
          puts "action_boot..."
          env.action_runner.run(VagrantPlugins::ProviderVirtualBox::Action::action_boot(), {
            machine: @machine,
            ui: @machine.ui,
          })
        end

        # puts "A", @machine.class  # Object
        # puts "B", @machine.instance_variables
        # puts "C", @machine.inspect

        # app = Vagrant::Action::Builder.new.tap do |b|
        #   b.use VagrantPlugins::ProviderVirtualBox::Action::Network
        # end
      
        # puts("running network upadte")
        # env.action_runner.run(app, {
        #       machine: @machine,
        #       ui: @machine.ui,
        #     })

        # comm.wait_for_ready(5000)
        # @machine.action('wait_for_communicator')
        # Vagrant::Action::Builder.new.tap do |b|
        #     b.use WaitForCommunicator, [:starting, :running]
        # end
        # Vagrant::Action::Builtin::WaitForCommunicator.new()

        # app = Vagrant::Action::Builder.new.tap do |b|
        #   b.use Vagrant::Action::Builtin::WaitForCommunicator, [:starting, :running]
        # end
    
        # Vagrant::Action.run(app)
        # runner  = Vagrant::Action::Runner.new
        # runner.run(app, env)

        # env.action_runner.run(app, {
        #       machine: @machine,
        #       ui: @machine.ui,
        #     })



        # begin
        #   sleep 5
        # end until @machine.communicate.ready?

        # puts "communicator ready"

        # comm.initialize(@machine)

        # @machine.communicator = klass.new(self)
        # @machine.communicator.initialize(@machine)

        # @machine.ui.info("trying action_boot...")
        # @machine.action('action_boot')

        # @machine.config.winrm.retry_limit = 1000
        # command = 'shutdown -t 0 -r -f'
        # @machine.ui.info("Issuing command: #{command}")
        # @machine.communicate.execute(command) do
        #   if type == :stderr
        #     @machine.ui.error(data);
        #   end
        # end
 
        # begin
        #   sleep 5
        # end until @machine.communicate.ready?
 
        # # Now the machine is up again, perform the necessary tasks.
        # @machine.ui.info("Launching remount_synced_folders action...")
        # @machine.action('remount_synced_folders')
      end
 
      # Nothing needs to be done on cleanup.
      def cleanup
        super
      end
    end
    SwitchToWinRMProvisioner
 
  end
end
